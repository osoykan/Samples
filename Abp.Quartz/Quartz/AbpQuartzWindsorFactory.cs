using Abp.Dependency;
using Abp.Extensions;

using Quartz;
using Quartz.Spi;

namespace Abp.Quartz.Quartz
{
    public class AbpQuartzWindsorFactory : IJobFactory
    {
        private readonly IIocManager iocManager;

        public AbpQuartzWindsorFactory(IIocManager iocManager)
        {
            this.iocManager = iocManager;
        }

        public IJob NewJob(TriggerFiredBundle bundle, IScheduler scheduler)
        {
            return iocManager.Resolve(bundle.JobDetail.JobType).As<IJob>();
        }

        public void ReturnJob(IJob job)
        {
            iocManager.Release(job);
        }
    }
}