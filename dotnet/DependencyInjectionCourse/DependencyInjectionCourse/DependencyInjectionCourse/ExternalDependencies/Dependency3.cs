namespace DependencyInjectionCourse.ExternalDependencies
{
    public class Dependency3 : IDependency3
    {
        public void Salute()
        {
            //nothing...
        }
    }

    public interface IDependency3
    {
        void Salute();
    }
}
