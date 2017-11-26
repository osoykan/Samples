using Core;
using Core.Bus;

using Products.Commands;

namespace Products.Services
{
    public interface IProductAppService : IHandles<CreateProductCommand>
    {
    }
}
