using System;
using System.Collections.Generic;
using System.Collections.Immutable;

using Core.Bus;

namespace Core.Aggregates
{
    public class AggregateRoot
    {
        protected AggregateRoot()
        {
            Events = new List<Event>();
        }

        private ICollection<Event> Events { get; }

        public Guid Id { get; protected set; }

        public override bool Equals(object obj)
        {
            return Id.Equals(((AggregateRoot)obj).Id);
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        protected void RaiseEvent(Event @event)
        {
            ApplyChange(@event);
            Events.Add(@event);
        }

        public ImmutableList<Event> GetUncommittedEvents()
        {
            return Events.ToImmutableList();
        }

        public void MarkEventsAsHandled()
        {
            Events.Clear();
        }

        protected void ApplyChange(Event @event)
        {
            ((dynamic)this).Apply(@event);
        }
    }
}
