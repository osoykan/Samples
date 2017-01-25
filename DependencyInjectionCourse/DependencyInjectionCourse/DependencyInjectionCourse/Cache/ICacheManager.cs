namespace DependencyInjectionCourse.Cache
{
    public interface ICacheManager
    {
        void Set(string key, object value);

        object Get(string key);
    }
}
