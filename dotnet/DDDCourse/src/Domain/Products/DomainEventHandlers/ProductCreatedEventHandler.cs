using System.Threading.Tasks;

using Core.Bus;

using Products.Events;

namespace Products.DomainEventHandlers
{
    public class ProductCreatedEventHandler : IHandles<ProductCreatedEvent>
    {
        public Task Handle(ProductCreatedEvent message)
        {
            //Do some domain event actions...
            //Pass message to other BoundedContexts or AggregateRoots

            return Task.CompletedTask;
        }
    }
}
