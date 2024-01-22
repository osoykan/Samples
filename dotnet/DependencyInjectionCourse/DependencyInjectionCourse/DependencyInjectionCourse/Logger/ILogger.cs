using JetBrains.Annotations;

namespace DependencyInjectionCourse.Logger
{
    public interface ILogger
    {
        void Log([NotNull] string message);
    }
}
