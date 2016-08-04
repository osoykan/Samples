using System.Reflection;

using Abp.Modules;

using AbpQuartzTask.HelloJob;

namespace Abp.Quartz.JobExecuter
{
    [DependsOn(
        typeof(HelloJobModule))]
    public class AbpQuartzJobExecuterModule : AbpModule
    {
        public override void Initialize()
        {
            IocManager.RegisterAssemblyByConvention(Assembly.GetExecutingAssembly());
        }
    }
}