using System;

using Abp.BackgroundJobs;
using Abp.Configuration.Startup;
using Abp.Dependency;
using Abp.Quartz.Quartz;
using Abp.Quartz.Quartz.Configuration;

namespace Abp.Quartz.Configuration
{
    public static class AbpQuartzConfigurationExtensions
    {
        public static IAbpQuartzConfiguration AbpQuartz(this IModuleConfigurations configurations)
        {
            return configurations.AbpConfiguration.Get<IAbpQuartzConfiguration>();
        }

        public static void UseQuartz(this IBackgroundJobConfiguration backgroundJobConfiguration, Action<IAbpQuartzConfiguration> configureAction = null)
        {
            backgroundJobConfiguration.AbpConfiguration.IocManager.RegisterIfNot<IQuartzScheduleJobManager, QuartzScheduleJobManager>();
            configureAction?.Invoke(backgroundJobConfiguration.AbpConfiguration.Modules.AbpQuartz());
        }
    }
}