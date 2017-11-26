namespace Core.Bus
{
    public interface IMessage
    {
    }

    public class Command : IMessage
    {
    }

    public class Event : IMessage
    {
        public int Version;
    }
}
