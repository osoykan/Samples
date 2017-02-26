using DependencyInjectionCourse.Cache;
using DependencyInjectionCourse.ExternalDependencies;
using DependencyInjectionCourse.Order;

using FluentAssertions;

using NSubstitute;

using Xunit;

namespace DependencyInjectionCourse.Tests
{
    /// <summary>
    ///     Simple and pure way to write unit tests.
    /// </summary>
    public class OrderService_Tests_1
    {
        [Fact]
        public void without_dependency_injection()
        {
            //-----------------------------------------------------------------------------------------------------------
            // Arrange
            //-----------------------------------------------------------------------------------------------------------
            var fakeCacheManager = Substitute.For<ICacheManager>();
            var fakeDependency1 = Substitute.For<IDependency1>();
            var fakeDependency2 = Substitute.For<IDependency2>();
            fakeCacheManager.Get("1").Returns(new Basket(1, 50));

            // Dependency1'in içindeki Logger'a test yazarken ihtiyaç duyulduğu anda tekrar bi New'leme yapılması gerek!
            // Dependency sayıları arttıkça New sayısı veya object passing artar, okunurluk azalır.
            var sut = new OrderService(fakeCacheManager, fakeDependency1, fakeDependency2);

            //-----------------------------------------------------------------------------------------------------------
            // Act
            //-----------------------------------------------------------------------------------------------------------
            OrderResult result = sut.DoOrder(1);

            //-----------------------------------------------------------------------------------------------------------
            // Assert
            //-----------------------------------------------------------------------------------------------------------
            result.BasketId.Should().Be(1);
            result.Total.Should().Be(50);
        }
    }
}
