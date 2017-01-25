namespace DependencyInjectionCourse
{
    public class NullCacheManager : ICacheManager
    {
        public static readonly NullCacheManager Instance = new NullCacheManager(); // Singleton null insntance.

        public void Set(string key, object value)
        {
            //do nothing!
        }

        public object Get(string key)
        {
            return null;
        }
    }
}
