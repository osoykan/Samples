using Quartz;

namespace Abp.Quartz.Quartz.Configuration
{
    public class AbpQuartzConfiguration : IAbpQuartzConfiguration
    {
        public IScheduler Scheduler { get; set; }
    }
}