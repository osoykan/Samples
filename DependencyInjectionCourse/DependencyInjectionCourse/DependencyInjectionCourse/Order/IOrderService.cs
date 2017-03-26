using JetBrains.Annotations;

namespace DependencyInjectionCourse.Order
{
    public interface IOrderService
    {
        [NotNull]
        OrderResult DoOrder(int basketId);
    }
}
