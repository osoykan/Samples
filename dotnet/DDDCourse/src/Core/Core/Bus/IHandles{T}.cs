using System.Threading.Tasks;

namespace Core.Bus
{
    public interface IHandles<T>
    {
        Task Handle(T message);
    }

    public interface ICommandSender
    {
        Task Send<T>(T command) where T : Command;
    }

    public interface IEventPublisher
    {
        Task Publish<T>(T @event) where T : Event;
    }

    public interface IBus : IEventPublisher, ICommandSender
    {
    }
}
