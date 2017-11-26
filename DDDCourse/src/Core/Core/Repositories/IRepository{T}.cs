using System;
using System.Collections.Generic;
using System.Threading.Tasks;

using Core.Aggregates;

namespace Core.Repositories
{
    public interface IRepository<T> where T : AggregateRoot
    {
        Task<T> Get(Func<T, bool> expression);

        Task<List<T>> GetAll();

        Task<List<T>> GetAll(Func<T, bool> expression);

        Task Save(T aggregate);
    }
}
