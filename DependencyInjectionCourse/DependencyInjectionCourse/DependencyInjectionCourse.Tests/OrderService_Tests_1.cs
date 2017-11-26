using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FakeItEasy;

using FluentAssertions;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Simple and pure way to write unit tests.
    /// </summary>
    public class OrderService_Tests_1
    {
        [Fact]
        public void order_should_be_done_successfully()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            var fakeCacheManager = A.Fake<ICacheManager>();
            var fakeDependency1 = A.Fake<IDependency1>();
            var fakeDependency2 = A.Fake<IDependency2>();
            A.CallTo(() => fakeCacheManager.Get("1")).Returns(new Basket(1, 50));

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            // Dependency1'in içindeki Logger'a test yazarken ihtiyaç duyulduğu anda tekrar bi New'leme yapılması gerek!
            // Dependency sayıları arttıkça New sayısı veya object passing artar, okunurluk azalır.
            var sut = new OrderService(fakeCacheManager, fakeDependency1, fakeDependency2);
            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
            A.CallTo(() => fakeDependency1.Salute()).MustHaveHappened();
        }
    }
}
