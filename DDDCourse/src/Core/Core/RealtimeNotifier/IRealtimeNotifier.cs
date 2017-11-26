using System.Threading.Tasks;

namespace Core.RealtimeNotifier
{
    public interface IRealtimeNotifier
    {
        Task Notify(string topic, string body);
    }
}
