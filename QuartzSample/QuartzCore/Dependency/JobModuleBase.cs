namespace QuartzCore.Dependency
{
    public abstract class JobModuleBase
    {
        public IocManager IocManager { get; set; }

        public virtual void BeforeCreateJob() {}

        public virtual void AfterCreateJob() {}
    }
}