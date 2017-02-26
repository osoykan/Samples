namespace DependencyInjectionCourse.ExternalDependencies
{
    public class Dependency2 : IDependency2
    {
        public void Salute()
        {
            // nothing...
        }
    }

    public interface IDependency2
    {
        void Salute();
    }
}
