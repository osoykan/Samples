using System.Data.Common;
using System.Data.Entity;

using Abp.EntityFramework;

using SampleAbpApplication.Entities;

namespace SampleAbpApplication.DbContexes
{
    public class AnimalDbContext : AbpDbContext
    {
        public AnimalDbContext() : base("Default")
        {
        }

        public AnimalDbContext(string nameOrConnectionString) : base(nameOrConnectionString)
        {
        }

        public AnimalDbContext(DbConnection existingConnection, bool contextOwnsConnection) : base(existingConnection, contextOwnsConnection)
        {
        }

        public virtual IDbSet<Animal> Animals { get; set; }
    }
}
