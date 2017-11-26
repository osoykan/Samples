using System.Collections.Generic;
using System.Linq;

using Core.Aggregates;

using Products.Entities;
using Products.Events;
using Products.Exceptions;
using Products.ValueObjects;

namespace Products
{
    public class Product : AggregateRoot
    {
        private const int MaxBarcodeLength = 40;

        private Product()
        {
            ProductImages = new List<ProductImage>();
        }

        public virtual string Name { get; private set; }

        public virtual string Barcode { get; private set; }

        public virtual string Code { get; private set; }

        public virtual ICollection<ProductImage> ProductImages { get; private set; }

        public virtual Price Price { get; private set; }

        public static Product Create(string name, string code, string barcode)
        {
            if (barcode.Length > MaxBarcodeLength)
            {
                throw new BarcodeExceedsMaximumLengthException($"Product creation for {barcode} exceeds maximum length of {MaxBarcodeLength}");
            }

            var product = new Product();
            product.RaiseEvent(new ProductCreatedEvent(barcode, code, name));
            return product;
        }

        internal void Apply(ProductCreatedEvent @event)
        {
            Name = @event.Name;
            Barcode = @event.Barcode;
            Code = @event.Code;
        }

        public void AddImage(string url, int order)
        {
            if (ProductImages.Any(x => x.ShowOrder == order)) { throw new BusinessException($"Image order already defined for Product:{Id}"); }

            RaiseEvent(
                ProductImage.ImageAddedToProduct(this, url, order)
            );
        }

        internal void Apply(ImageAddedEvent @event)
        {
            var image = new ProductImage();
            image.Route(@event);
            ProductImages.Add(image);
        }
    }
}
