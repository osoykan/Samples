using DependencyInjectionCourse.Logger;

using JetBrains.Annotations;

namespace DependencyInjectionCourse.ExternalDependencies
{
    public class Dependency1 : IDependency1
    {
        private readonly IDependency3 _dependency3;
        private readonly ILogger _logger;

        public Dependency1([NotNull] IDependency3 dependency3, [NotNull] ILogger logger)
        {
            _dependency3 = dependency3;
            _logger = logger;
        }

        public void Salute()
        {
            _logger.Log($"Hello from {nameof(Dependency1)}");
        }
    }

    public interface IDependency1
    {
        void Salute();
    }
}
