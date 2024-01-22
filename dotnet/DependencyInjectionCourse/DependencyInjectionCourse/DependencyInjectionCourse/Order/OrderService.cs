using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;

using JetBrains.Annotations;

namespace DependencyInjectionCourse.Order
{
    public class OrderService : IOrderService
    {
        private readonly ICacheManager _cacheManager;
        private readonly IDependency1 _dependency1;
        private readonly IDependency2 _dependency2;

        public OrderService([NotNull] ICacheManager cacheManager, [NotNull] IDependency1 dependency1, [NotNull] IDependency2 dependency2)
        {
            _cacheManager = cacheManager;
            _dependency1 = dependency1;
            _dependency2 = dependency2;
        }

        public OrderResult DoOrder(int basketId)
        {
            var basket = (Basket)_cacheManager.Get(basketId.ToString());
            _dependency1.Salute();
            return new OrderResult { Total = basket.Total, BasketId = basket.Id };
        }
    }
}
