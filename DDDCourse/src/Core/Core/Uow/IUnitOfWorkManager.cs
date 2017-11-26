using System.Transactions;

namespace Core.Uow
{
    public interface IUnitOfWorkManager
    {
        IUnitOfWork Begin(IsolationLevel isolationLevel);
    }
}
