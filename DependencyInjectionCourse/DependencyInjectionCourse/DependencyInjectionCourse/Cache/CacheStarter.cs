using Autofac;

using JetBrains.Annotations;

namespace DependencyInjectionCourse.Cache
{
    public class CacheStarter : IStartable
    {
        private readonly CacheManager _cacheManager;

        public CacheStarter([NotNull] CacheManager cacheManager)
        {
            _cacheManager = cacheManager;
        }

        public void Start()
        {
            _cacheManager.Set(1.ToString(), new Basket(1, 50));
        }
    }
}
