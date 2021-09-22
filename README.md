# Bilder

[![](https://jitpack.io/v/kshitij6325/BilderDemo.svg)](https://jitpack.io/#kshitij6325/bilder)

## Download
Add following to your project's build.gradle
```
allprojects {
  repositories {
      ...			
    maven { url 'https://jitpack.io' }
  }
}
```
Add following to your module's build.gralde
```
dependencies {
  implementation 'com.github.kshitij6325:bilder:latest-version'
}
```
## How to use?
```
override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Bilder.load(
            holder.image.context as Activity,
            source = Source.Url(list[position].second),
            imageView = holder.image,
            onBitmapLoadFailure = {
            }
        )
    }
```
