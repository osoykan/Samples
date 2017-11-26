using System.Threading.Tasks;

using Core.Bus;

using Microsoft.AspNetCore.Mvc;

using Products.Commands;

namespace Products
{
    [Route("products")]
    public class ProductController : ControllerBase
    {
        private readonly ICommandSender _bus;

        public ProductController(IBus bus)
        {
            _bus = bus;
        }

        [Route("create")]
        [HttpPost]
        public Task Create(CreateProductCommand command)
        {
            return _bus.Send(command);
        }
    }
}
