using System;

using Abp.BackgroundJobs;
using Abp.Configuration.Startup;
using Abp.Dependency;
using Abp.Quartz.Quartz;
using Abp.Quartz.Quartz.Configuration;

using Quartz;
using Quartz.Spi;

namespace Abp.Quartz.Configuration
{
    public static class AbpQuartzConfigurationExtensions
    {
        public static IAbpQuartzConfiguration AbpQuartz(this IModuleConfigurations configurations)
        {
            return configurations.AbpConfiguration.Get<IAbpQuartzConfiguration>();
        }

        public static void UseQuartz(this IBackgroundJobConfiguration backgroundJobConfiguration, Action<IAbpQuartzConfiguration> configureAction)
        {
            backgroundJobConfiguration.AbpConfiguration.IocManager.RegisterIfNot<IJobFactory, AbpQuartzWindsorFactory>();
            backgroundJobConfiguration.AbpConfiguration.IocManager.RegisterIfNot<IJobListener, AbpQuartzJobListener>();
            configureAction(backgroundJobConfiguration.AbpConfiguration.Modules.AbpQuartz());
        }
    }
}