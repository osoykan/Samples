using System.Threading.Tasks;

namespace Core.Mailing
{
    public interface IMailSender
    {
        Task Send(string body, string subject, params string[] to);
    }
}
