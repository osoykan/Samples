using Quartz;

namespace Abp.Quartz.Quartz
{
    public interface IAbpQuartzJob
    {
        JobBuilder JobBuilder { get; set; }

        IJobDetail JobDetail { get; }

        ITrigger Trigger { get; set; }

        void Validate();
    }
}