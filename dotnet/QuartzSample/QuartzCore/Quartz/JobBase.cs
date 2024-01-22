using System;

using Quartz;

using QuartzCore.Dependency;

namespace QuartzCore.Quartz
{
    public abstract class JobBase : IPayflexJob
    {
        public IocManager IocManager { get; set; }

        protected JobBuilder JobBuilder { get; set; }

        public IJobDetail JobDetail {get; private set; }

        public ITrigger Trigger { get; set; }

        public virtual void CreateJobDetail()
        {
            JobBuilder = JobBuilder.Create(GetType());
        }

        public abstract void CreateJobTrigger();

        public void Validate()
        {
            if (JobBuilder == null)
                throw new ArgumentNullException($"Job detail has not been defined, please define it correctly!.");

            if (JobDetail == null)
                throw new ArgumentNullException($"Job detail has not been defined, please define it correctly!.");

            if (Trigger == null)
                throw new ArgumentNullException($"Job trigger was not found, please define it correctly!.");
        }

        public void Build()
        {
            JobDetail = JobBuilder.Build();
        }
    }
}