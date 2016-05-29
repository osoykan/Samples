using QuartzCore.Dependency;

namespace QuartzCore.Jobs
{
    public class HelloJobModule : JobModuleBase
    {
        public override void BeforeCreateJob()
        {
            IocManager.Register<IDummy, Dummy>(LifeStyle.Transient);
        }
    }
}