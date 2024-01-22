using System.Threading.Tasks;

namespace Core.Logging
{
    public interface ILogger
    {
        Task Log(string message);
    }
}
