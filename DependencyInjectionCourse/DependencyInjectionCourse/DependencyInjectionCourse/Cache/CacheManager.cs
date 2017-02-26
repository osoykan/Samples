using System.Collections.Concurrent;

using JetBrains.Annotations;

namespace DependencyInjectionCourse.Cache
{
    public class CacheManager : ICacheManager
    {
        private static readonly ConcurrentDictionary<string, object> _cache = new ConcurrentDictionary<string, object>();

        public void Set(string key, object value)
        {
            Check.NotNull(key, nameof(key));
            Check.NotNull(value, nameof(value));
            _cache.TryAdd(key, value);
        }

        public object Get(string key)
        {
            Check.NotNull(key, nameof(key));

            object value;
            _cache.TryGetValue(key, out value);
            return value;
        }
    }
}
