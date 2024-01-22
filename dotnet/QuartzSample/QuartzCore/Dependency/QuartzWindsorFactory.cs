using Castle.MicroKernel;

using Quartz;
using Quartz.Spi;

namespace QuartzCore.Dependency
{
    public class QuartzWindsorFactory : IJobFactory
    {
        public IKernel Kernel { get; set; }

        public IJob NewJob(TriggerFiredBundle bundle, IScheduler scheduler)
        {
            return (IJob)Kernel.Resolve(bundle.JobDetail.JobType);
        }

        public void ReturnJob(IJob job)
        {
            Kernel.ReleaseComponent(job);
        }
    }
}