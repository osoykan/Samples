using System.Reflection;

using Abp;
using Abp.Dapper;
using Abp.EntityFramework;
using Abp.Modules;

namespace SampleAbpApplication
{
    [DependsOn(
        typeof(AbpEntityFrameworkModule),
        typeof(AbpKernelModule),
        typeof(AbpDapperModule)
    )]
    public class SampleApplicationModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }
    }
}
