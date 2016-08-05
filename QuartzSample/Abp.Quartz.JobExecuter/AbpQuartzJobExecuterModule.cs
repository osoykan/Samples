using System.Reflection;

using Abp.Modules;

using AbpQuartzTask.GoodbyeJob;
using AbpQuartzTask.HelloJob;

namespace Abp.Quartz.ConsoleApp
{
    [DependsOn(
        typeof(HelloJobModule),
        typeof(GoodbyeJobModule))]
    public class AbpQuartzJobExecuterModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }
    }
}