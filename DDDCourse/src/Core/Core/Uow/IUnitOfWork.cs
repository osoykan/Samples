using System;
using System.Threading.Tasks;

namespace Core.Uow
{
    public interface IUnitOfWork : IDisposable
    {
        Task Complete();
    }
}
