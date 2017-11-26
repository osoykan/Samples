using System.Threading.Tasks;
using System.Transactions;

using Core.Authorization;
using Core.Mailing;
using Core.RealtimeNotifier;
using Core.Session;
using Core.Uow;

using Products.Commands;

namespace Products.Services
{
    public class ProductAppService : IProductAppService
    {
        private readonly IAuthorizationService _authorizationService;
        private readonly IProductDomainService _productDomainService;
        private readonly IUnitOfWorkManager _unitOfWorkManager;
        private readonly IMailSender _mailSender;
        private readonly ISession _session;
        private readonly IRealtimeNotifier _realtimeNotifier;

        public ProductAppService(
            IProductDomainService productDomainService,
            IAuthorizationService authorizationService,
            IUnitOfWorkManager unitOfWorkManager,
            IMailSender mailSender,
            ISession session, 
            IRealtimeNotifier realtimeNotifier)
        {
            _productDomainService = productDomainService;
            _authorizationService = authorizationService;
            _unitOfWorkManager = unitOfWorkManager;
            _mailSender = mailSender;
            _session = session;
            _realtimeNotifier = realtimeNotifier;
        }

        public async Task Handle(CreateProductCommand message)
        {
            _authorizationService.CheckPermission("User.CreateProduct");

            message.Validate();

            using (IUnitOfWork unitOfWork = _unitOfWorkManager.Begin(IsolationLevel.ReadCommitted))
            {
                await _productDomainService.Create(message.Name, message.Code, message.Barcode);

                await unitOfWork.Complete();
            }

            await _mailSender.Send($"Hello {_session.Username}, product is created for you.", "Product Creation", _session.Email);

            await _realtimeNotifier.Notify("ProductCreated", message.ToString());
        }
    }
}
