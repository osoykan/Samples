using System;
using System.Collections.Generic;

using Core.Bus;

namespace Core.Entities
{
    public class Entity
    {
        private readonly Dictionary<Type, Action<object>> _router;

        public Entity()
        {
            _router = new Dictionary<Type, Action<object>>();
        }

        public Guid Id { get; protected set; }

        public override bool Equals(object obj)
        {
            return Id.Equals(((Entity)obj).Id);
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public void Register<TEvent>(Action<TEvent> handler) where TEvent : Event
        {
            if (!_router.TryGetValue(typeof(TEvent), out Action<object> handle))
            {
                _router.Add(typeof(TEvent), @event => handler((TEvent)@event));
            }
        }

        public void Route(object @event)
        {
            if (_router.TryGetValue(@event.GetType(), out Action<object> handler))
            {
                handler(@event);
            }
        }
    }
}
