using System;

using Core.Entities;

using Products.Events;

namespace Products.Entities
{
    public class ProductImage : Entity
    {
        public ProductImage()
        {
            Register<ImageAddedEvent>(@event =>
            {
                Product = @event.Product;
                ImageUrl = @event.Url;
                ShowOrder = @event.Order;
            });
        }

        public virtual Product Product { get; private set; }

        public virtual string ImageUrl { get; private set; }

        public virtual int ShowOrder { get; private set; }

        public static ImageAddedEvent ImageAddedToProduct(Product product, string url, int order)
        {
            if (string.IsNullOrEmpty(url))
            {
                throw new ArgumentNullException("url");
            }

            if (order <= 0)
            {
                throw new ArgumentException("order cannot be 0 or less");
            }

            return new ImageAddedEvent(product, url, order);
        }
    }
}
