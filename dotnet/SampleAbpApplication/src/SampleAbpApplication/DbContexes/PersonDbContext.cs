using System.Data.Common;
using System.Data.Entity;

using Abp.EntityFramework;

using SampleAbpApplication.Entities;

namespace SampleAbpApplication.DbContexes
{
    public class PersonDbContext : AbpDbContext
    {
        public PersonDbContext() : base("Default")
        {
        }

        protected PersonDbContext(string nameOrConnectionString) : base(nameOrConnectionString)
        {
        }

        public PersonDbContext(DbConnection existingConnection, bool contextOwnsConnection) : base(existingConnection, contextOwnsConnection)
        {
        }

        public virtual IDbSet<Person> Persons { get; set; }
    }
}
