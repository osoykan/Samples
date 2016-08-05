using System;

using Quartz;

namespace Abp.Quartz.Quartz
{
    public abstract class AbpQuartzJobBase : IAbpQuartzJob
    {
        public void Validate()
        {
            if (JobBuilder == null)
                throw new ArgumentNullException($"Job detail has not been defined, please define it correctly!.");

            if (JobDetail == null)
                throw new ArgumentNullException($"Job detail has not been defined, please define it correctly!.");

            if (Trigger == null)
                throw new ArgumentNullException($"Job trigger was not found, please define it correctly!.");
        }

        public JobBuilder JobBuilder { get; set; }

        public IJobDetail JobDetail { get; private set; }

        public ITrigger Trigger { get; set; }

        public IAbpQuartzJob Build()
        {
            CreateJobDetail();
            CreateJobTrigger();
            JobDetail = JobBuilder.Build();
            Validate();
            return this;
        }

        public virtual void CreateJobDetail()
        {
            JobBuilder = JobBuilder.Create(GetType());
        }

        public abstract void CreateJobTrigger();
    }
}