using Autofac;

namespace DependencyInjectionCourse
{
    public class CacheStarter : IStartable
    {
        private readonly ICacheManager _cacheManager;

        public CacheStarter(ICacheManager cacheManager)
        {
            _cacheManager = cacheManager;
        }

        public void Start()
        {
            _cacheManager.Set(1.ToString(), new Basket(1, 50));
        }
    }
}
