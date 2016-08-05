using QuartzCore.Dependency;

using QuartzTask.HelloJob.Dummy;

namespace QuartzTask.HelloJob
{
    public class HelloJobModule : JobModuleBase
    {
        public override void BeforeCreateJob()
        {
            IocManager.Register<IDummy, Dummy.Dummy>(LifeStyle.Transient);
        }
    }
}