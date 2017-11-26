using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Threading.Tasks;

using Core;
using Core.Bus;
using Core.Repositories;

namespace Products.Repositories
{
    public class ProductRepository : IRepository<Product>
    {
        private readonly IEventPublisher _bus;
        private readonly ConcurrentBag<Product> _db = new ConcurrentBag<Product>();

        public ProductRepository(IEventPublisher bus)
        {
            _bus = bus;
        }

        public Task<Product> Get(Func<Product, bool> expression)
        {
            Product product = _db.FirstOrDefault(expression);

            return Task.FromResult(product);
        }

        public Task<List<Product>> GetAll()
        {
            return Task.FromResult(_db.ToList());
        }

        public Task<List<Product>> GetAll(Func<Product, bool> expression)
        {
            return Task.FromResult(_db.Where(expression).ToList());
        }

        public Task Save(Product aggregate)
        {
            ImmutableList<Event> events = aggregate.GetUncommittedEvents();
            
            _db.Add(aggregate);

            events.ForEach(@event => _bus.Publish(@event));

            aggregate.MarkEventsAsHandled();

            return Task.FromResult(0);
        }
    }
}
