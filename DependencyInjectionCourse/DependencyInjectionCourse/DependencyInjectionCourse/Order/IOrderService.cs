using JetBrains.Annotations;

namespace DependencyInjectionCourse
{
    public interface IOrderService
    {
        [NotNull] OrderResult DoOrder(int basketId);
    }
}
