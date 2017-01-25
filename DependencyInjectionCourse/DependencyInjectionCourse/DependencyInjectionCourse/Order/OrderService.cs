using DependencyInjectionCourse.Cache;

using JetBrains.Annotations;

namespace DependencyInjectionCourse.Order
{
    public class OrderService : IOrderService
    {
        private readonly ICacheManager _cacheManager;

        public OrderService([NotNull] ICacheManager cacheManager)
        {
            _cacheManager = cacheManager;
        }

        public OrderResult DoOrder(int basketId)
        {
            var basket = (Basket)_cacheManager.Get(basketId.ToString());
            return new OrderResult { Total = basket.Total, BasketId = basket.Id };
        }
    }
}
