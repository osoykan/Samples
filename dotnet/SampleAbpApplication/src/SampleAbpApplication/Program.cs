using System.Data.Entity;

using Abp;

using HibernatingRhinos.Profiler.Appender.EntityFramework;

using SampleAbpApplication.DbContexes;

namespace SampleAbpApplication
{
    public class Program
    {
        public static void Main(string[] args)
        {
            EntityFrameworkProfiler.Initialize();

            Database.SetInitializer(new NullDatabaseInitializer<PersonDbContext>());
            Database.SetInitializer(new NullDatabaseInitializer<AnimalDbContext>());

            AbpBootstrapper bootstrapper = AbpBootstrapper.Create<SampleApplicationModule>();
            bootstrapper.Initialize();

            var someDomainService = bootstrapper.IocManager.Resolve<SomeDomainService>();
            someDomainService.DoSomeStuff();
        }
    }
}
