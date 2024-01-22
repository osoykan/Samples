using JetBrains.Annotations;

namespace DependencyInjectionCourse.Cache
{
    public interface ICacheManager
    {
        void Set([NotNull] string key, [NotNull] object value);

        object Get([NotNull] string key);
    }
}
