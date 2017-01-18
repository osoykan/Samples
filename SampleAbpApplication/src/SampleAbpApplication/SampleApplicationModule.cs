using System.Reflection;

using Abp;
using Abp.Configuration.Startup;
using Abp.Dependency;
using Abp.EntityFramework;
using Abp.EntityFramework.Uow;
using Abp.Modules;

namespace SampleAbpApplication
{
    [DependsOn(
        typeof(AbpEntityFrameworkModule),
        typeof(AbpKernelModule)
    )]
    public class SampleApplicationModule : AbpModule
    {
        public override void PreInitialize()
        {
            Configuration.ReplaceService<IEfTransactionStrategy, DbContextEfTransactionStrategy>(DependencyLifeStyle.Transient);
        }

        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }
    }
}
