using System.Threading.Tasks;

using Core.Repositories;

using Products.Exceptions;

namespace Products.Services
{
    public class ProductDomainService : IProductDomainService
    {
        private readonly IRepository<Product> _repository;

        public ProductDomainService(IRepository<Product> repository)
        {
            _repository = repository;
        }

        public async Task Create(string name, string code, string barcode)
        {
            Product existing = await _repository.Get(x => x.Barcode == barcode);

            if (existing != null) throw new AggregateDuplicatedException($"Duplicate Product creation attempt for {barcode}");

            Product product = Product.Create(name, code, barcode);

            await _repository.Save(product);
        }
    }
}
